package com.fullstack.Backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Platform")
public class Platform extends BaseEntity{
	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String version;
}
