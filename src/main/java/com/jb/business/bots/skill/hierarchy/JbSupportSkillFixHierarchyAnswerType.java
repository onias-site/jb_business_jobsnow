package com.jb.business.bots.skill.hierarchy;

/**
 * Kinds of answer the operator gives along the {@code fixSkillHierarchy} command: a decision on the items
 * ({@code approve}, {@code reject}, {@code oneByOne}), the request to ignore the user ({@code ignore}) and the
 * answer to its confirmation ({@code yes}, {@code no}).
 */
enum JbSupportSkillFixHierarchyAnswerType {
	approve,
	reject,
	oneByOne,
	ignore,
	yes,
	no,
	notUnderstood
}
