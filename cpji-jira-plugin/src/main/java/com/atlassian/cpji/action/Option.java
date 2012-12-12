package com.atlassian.cpji.action;

/**
 * @since v3.0
 */
public class Option {
	private final String value;
	private final boolean selected;
	private String label;

	Option(String value, boolean selected) {
		this.value = value;
		this.selected = selected;
	}

	Option(String value, boolean selected, final String label) {
		this.value = value;
		this.selected = selected;
		this.label = label;
	}

	public String getValue() {
		return value;
	}

	public boolean isSelected() {
		return selected;
	}

	public String getLabel() {
		return label;
	}
}
