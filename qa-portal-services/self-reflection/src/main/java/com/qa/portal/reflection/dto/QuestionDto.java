package com.qa.portal.reflection.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qa.portal.common.dto.QaBaseDto;

public class QuestionDto extends QaBaseDto {

	private final Integer id;

	private final String body;

	private final String Category;

	private final Set<ReflectionDto> forms;

	@JsonCreator
	public QuestionDto(@JsonProperty Integer id, @JsonProperty String body, @JsonProperty String category,
			@JsonProperty Set<ReflectionDto> forms) {
		super();
		this.id = id;
		this.body = body;
		Category = category;
		this.forms = forms;
	}

	public Integer getId() {
		return id;
	}

	public String getBody() {
		return body;
	}

	public String getCategory() {
		return Category;
	}

	public Set<ReflectionDto> getForms() {
		return forms;
	}

}
