package com.jb.business.bots.skill.hierarchy;

import com.jn.messages.JnSystemMessage;

/**
 * Texts sent to the support bot operator along the {@code fixSkillHierarchy} command, stored in
 * {@link JnEntitySystemMessage} and seeded by {@code JbEntityBot}. The placeholders of the templates are the
 * fields of {@link JbSupportSkillFixHierarchyMessageFields} and of the pending request ({@code email},
 * {@code parent}, {@code skill}).
 */
public enum JbSupportSkillFixHierarchyMessages implements JnSystemMessage {

	/** The options the operator has to decide the pending items. */
	options,

	/** Asks the operator to confirm that the user will be ignored: {@code {email}}. */
	ignoreConfirmation,

	/** Justification that goes to the user for an item approved in an earlier review. */
	previousDecisionApproved,

	/** Justification that goes to the user for an item rejected in an earlier review. */
	previousDecisionRejected,

	/** Answer the operator gave that is not a decision with justification; precedes the question asked again. */
	notUnderstood,

	/**
	 * Asks the operator to decide one item: {@code {itemNumber}}, {@code {itemsCount}}, {@code {skill}},
	 * {@code {typeDescription}} and {@code {parent}}.
	 */
	itemPrompt,

	/**
	 * Summary of the finished review: {@code {email}}, {@code {parent}}, {@code {approvedSkills}} and
	 * {@code {rejectedSkills}}.
	 */
	reviewFinished,

	/** Shown in the summary in place of the skills when no item got the decision. */
	noSkill,

	/** The operator confirmed that the user is ignored: {@code {email}} and {@code {parent}}. */
	userIgnored,

	/** The operator gave up ignoring the user and goes back to the {@code {options}}. */
	ignoreCanceled,

	/** Answer to the ignore confirmation that is neither yes nor no; asks the {@code {ignoreConfirmation}} again. */
	ignoreConfirmationNotUnderstood,

	/** Header of the request shown to the operator: {@code {typeDescription}}, {@code {email}} and {@code {parent}}. */
	requestHeader,

	/** Opens the part of one type of the request: {@code {typeDescription}} and the user's {@code {description}}. */
	requestTypeJustification,

	/** The items of the type still to be decided: {@code {skills}}. */
	pendingItems,

	/** The items of the type approved in an earlier review, which will not be asked: {@code {skills}}. */
	approvedBefore,

	/** The items of the type rejected in an earlier review, which will not be asked: {@code {skills}}. */
	rejectedBefore,
	;
}
