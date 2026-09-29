package com.jb.business.bots.skill.hierarchy;

/**
 * Steps of the {@code fixSkillHierarchy} command after the first one (the first step has the name of the
 * command itself): choosing how to decide the items and deciding them one by one.
 */
public enum JbSupportSkillFixHierarchySteps {
	fixSkillHierarchyChooseMode,
	fixSkillHierarchyDecideItem
}
