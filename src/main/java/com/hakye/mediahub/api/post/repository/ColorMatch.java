package com.hakye.mediahub.api.post.repository;

import java.util.UUID;

/** A post that matched a color search and how far its closest stored color is (CIELAB distance). */
public interface ColorMatch {
    UUID getPostId();

    Double getDistance();
}