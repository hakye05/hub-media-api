package com.hakye.mediahub.api.post.domain;

import com.hakye.mediahub.api.post.color.ColorLab;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "post_colors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostColor {

    public static final int MAX_PER_POST = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "post_id", nullable = false, updatable = false)
    private UUID postId;

    @Column(nullable = false, updatable = false)
    private Integer position;

    @Column(nullable = false, updatable = false, length = 7)
    private String hex;

    @Column(name = "lab_l", nullable = false, updatable = false)
    private Double labL;

    @Column(name = "lab_a", nullable = false, updatable = false)
    private Double labA;

    @Column(name = "lab_b", nullable = false, updatable = false)
    private Double labB;

    private PostColor(UUID postId, int position, String hex) {
        ColorLab lab = ColorLab.fromHex(hex);
        this.postId = postId;
        this.position = position;
        this.hex = hex;
        this.labL = lab.l();
        this.labA = lab.a();
        this.labB = lab.b();
    }

    /** Normalized, de-duplicated and capped at {@link #MAX_PER_POST}; position 0 is the most dominant. */
    public static List<PostColor> listOf(UUID postId, List<String> hexes) {
        if (hexes == null) return List.of();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String raw : hexes) unique.add(ColorLab.normalizeHex(raw));

        List<PostColor> out = new ArrayList<>();
        for (String hex : unique) {
            if (out.size() == MAX_PER_POST) break;
            out.add(new PostColor(postId, out.size(), hex));
        }
        return out;
    }
}