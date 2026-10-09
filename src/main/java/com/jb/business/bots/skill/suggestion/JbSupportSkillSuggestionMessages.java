package com.jb.business.bots.skill.suggestion;

import com.jn.messages.JnSystemMessage;

/**
 * Texts sent to the support bot operator along the {@code reviewSkillSuggestion} command, stored in
 * {@code JnEntitySystemMessage} and seeded by {@code JbEntityBot}. The placeholders of the templates are the fields
 * of {@link JbSupportSkillSuggestionMessageFields} and of the pending suggestion ({@code email}, {@code skill},
 * {@code description}).
 */
public enum JbSupportSkillSuggestionMessages implements JnSystemMessage {

	/**
	 * The suggestion shown to the operator: {@code {email}}, {@code {skill}}, {@code {synonymNames}},
	 * {@code {description}} and the {@code {options}}.
	 */
	request,

	/** The options the operator has to decide the suggestion. */
	options,

	/** Answer that is not a decision with a justification of 10 to 500 characters; precedes the options asked again. */
	notUnderstood,

	/** The skill was approved: {@code {email}} and {@code {skill}}. */
	approved,

	/** The skill was rejected: {@code {email}} and {@code {skill}}. */
	rejected,

	/** Asks the operator to confirm that the user will be ignored: {@code {email}}. */
	ignoreConfirmation,

	/** Answer to the ignore confirmation that is neither yes nor no; asks the {@code {ignoreConfirmation}} again. */
	ignoreConfirmationNotUnderstood,

	/** The operator gave up ignoring the user and goes back to the {@code {options}}. */
	ignoreCanceled,

	/** The operator confirmed that the user is ignored: {@code {email}} and {@code {skill}}. */
	userIgnored,
	;
}
