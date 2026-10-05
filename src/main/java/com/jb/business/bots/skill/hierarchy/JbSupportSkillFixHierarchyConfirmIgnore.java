package com.jb.business.bots.skill.hierarchy;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillFixHierarchyIgnoreUser;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Step of the {@code fixSkillHierarchy} command that confirms the intention of the operator to ignore the user. Yes
 * records the user in {@code VisEntityCommandNotAllowedToUser}, discards the request without notifying the user and ends
 * the session ({@code userIgnored}); no goes back to the choice of how to decide the items ({@code ignoreCanceled}); any
 * other answer asks again ({@code invalidAnswer}).
 */
public class JbSupportSkillFixHierarchyConfirmIgnore implements CcpBusiness {

	/**
	 * Handles the confirmation.
	 * @param json the session
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code userIgnored}, {@code ignoreCanceled} or {@code invalidAnswer}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		JbSupportSkillFixHierarchyAnswer answer = JbSupportSkillFixHierarchyAnswer.read(json);
		JnLanguage language = JbSupportSkillFixHierarchyConversation.getLanguage(json);

		if(answer.isYes()) {
			VisBusinessSkillFixHierarchyIgnoreUser.INSTANCE.execute(json);

			CcpJsonRepresentation userIgnoredParameters = json.getJsonPiece(VisEntitySkillFixHierarchyPending.Fields.email, VisEntitySkillFixHierarchyPending.Fields.parent);
			String userIgnoredText = JbSupportSkillFixHierarchyMessages.userIgnored.getMessage(language, userIgnoredParameters);
			CcpJsonRepresentation jsonWithReply = json.put(JbSupportSkillFixHierarchyFields.botReply, userIgnoredText);
			CcpJsonRepresentation userIgnored = JbSupportSkillFixHierarchyStatus.userIgnored.throwException(jsonWithReply);
			return userIgnored;
		}

		if(answer.isNo()) {
			String options = JbSupportSkillFixHierarchyConversation.getOptions(language);
			CcpJsonRepresentation ignoreCanceledParameters = CcpOtherConstants.EMPTY_JSON.put(JbSupportSkillFixHierarchyMessageFields.options, options);
			String backToOptions = JbSupportSkillFixHierarchyMessages.ignoreCanceled.getMessage(language, ignoreCanceledParameters);
			CcpJsonRepresentation jsonWithReply = json.put(JbSupportSkillFixHierarchyFields.botReply, backToOptions);
			CcpJsonRepresentation ignoreCanceled = JbSupportSkillFixHierarchyStatus.ignoreCanceled.throwException(jsonWithReply);
			return ignoreCanceled;
		}

		String ignoreConfirmation = JbSupportSkillFixHierarchyConversation.getIgnoreConfirmation(json);
		CcpJsonRepresentation askAgainParameters = CcpOtherConstants.EMPTY_JSON.put(JbSupportSkillFixHierarchyMessageFields.ignoreConfirmation, ignoreConfirmation);
		String askAgain = JbSupportSkillFixHierarchyMessages.ignoreConfirmationNotUnderstood.getMessage(language, askAgainParameters);
		CcpJsonRepresentation jsonAskingAgain = json.put(JbSupportSkillFixHierarchyFields.botReply, askAgain);
		CcpJsonRepresentation askedAgain = JbSupportSkillFixHierarchyStatus.invalidAnswer.throwException(jsonAskingAgain);
		return askedAgain;
	}
}
