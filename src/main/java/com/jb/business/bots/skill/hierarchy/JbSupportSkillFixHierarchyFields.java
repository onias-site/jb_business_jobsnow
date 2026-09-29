package com.jb.business.bots.skill.hierarchy;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Session fields of the {@code fixSkillHierarchy} command: {@code botReply} is the text each step sends to the
 * operator (the end message and the flow messages of the steps are just {@code {botReply}}), {@code reviewItems}
 * are the pending items of the request ({@code type} and {@code skill}) in the order they are shown, and
 * {@code itemIndex} is the item being decided in the one by one review.
 */
public enum JbSupportSkillFixHierarchyFields implements CcpJsonFieldName{
	botReply,
	reviewItems,
	itemIndex
}
