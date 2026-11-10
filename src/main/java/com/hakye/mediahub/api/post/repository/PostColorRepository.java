package com.hakye.mediahub.api.post.repository;

import com.hakye.mediahub.api.post.domain.PostColor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostColorRepository extends JpaRepository<PostColor, UUID> {
}