package com.jb.business.bots.skill.hierarchy;

/**
 * Kinds of answer the operator gives along the {@code fixSkillHierarchy} command: a decision on the items
 * ({@code approve}, {@code reject}, {@code oneByOne}), the request to ignore the user ({@code ignore}) and the
 * answer to its confirmation ({@code yes}, {@code no}).
 */
enum JbSupportSkillFixHierarchyAnswerType {
	/** Approve, with a justification. */
	approve,
	/** Reject, with a justification. */
	reject,
	/** Decide item by item. */
	oneByOne,
	/** Ignore the user for the command. */
	ignore,
	/** Confirm. */
	yes,
	/** Decline. */
	no,
	/** Anything else. */
	notUnderstood
}
