package com.jb.business.bots.skill.hierarchy;

/**
 * Steps of the {@code fixSkillHierarchy} command after the first one (the first step has the name of the
 * command itself): choosing how to decide the items, deciding them one by one and confirming that the user
 * will be ignored for the command.
 */
public enum JbSupportSkillFixHierarchySteps {
	/** Choosing how to decide the items. */
	fixSkillHierarchyChooseMode,
	/** Deciding the items one by one. */
	fixSkillHierarchyDecideItem,
	/** Confirming that the user will be ignored. */
	fixSkillHierarchyConfirmIgnore
}
