package com.jb.business.bots.skill.suggestion;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Placeholders of the {@link JbSupportSkillSuggestionMessages} templates that are not fields of the pending
 * suggestion.
 */
public enum JbSupportSkillSuggestionMessageFields implements CcpJsonFieldName{
	/** The {@code synonymNames} field: the synonyms of the suggestion, separated by commas. */
	synonymNames,
	/** The {@code options} field. */
	options,
	/** The {@code ignoreConfirmation} field. */
	ignoreConfirmation
}
