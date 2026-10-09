package com.jb.business.bots.skill.suggestion;

/**
 * Steps of the {@code reviewSkillSuggestion} command after the first one (the first step has the name of the
 * command itself): deciding the suggestion and confirming that the user will be ignored for the command.
 */
public enum JbSupportSkillSuggestionSteps {
	/** Approving, rejecting or asking to ignore the user. */
	reviewSkillSuggestionDecide,
	/** Confirming that the user will be ignored. */
	reviewSkillSuggestionConfirmIgnore
}
