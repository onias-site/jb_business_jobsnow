package com.jb.business.bots.skill.suggestion;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyAnswer;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillSuggestionIgnoreUser;
import com.vis.entities.VisEntitySkillPending;

/**
 * Step of the {@code reviewSkillSuggestion} command that confirms the intention of the operator to ignore the user.
 * Yes records the user in {@code VisEntityCommandNotAllowedToUser}, discards the suggestion without notifying the
 * user and ends the session ({@code userIgnored}); no goes back to the decision ({@code ignoreCanceled}); any other
 * answer asks again ({@code invalidAnswer}).
 */
public class JbSupportSkillSuggestionConfirmIgnore implements CcpBusiness {

	/**
	 * Handles the confirmation.
	 * @param json the session
	 * @return never returns normally
	 * @throws CcpErrorFlowDisturb with {@code userIgnored}, {@code ignoreCanceled} or {@code invalidAnswer}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		JbSupportSkillFixHierarchyAnswer answer = JbSupportSkillFixHierarchyAnswer.read(json);
		JnLanguage language = JbSupportSkillSuggestionConversation.getLanguage(json);

		if(answer.isYes()) {
			VisBusinessSkillSuggestionIgnoreUser.INSTANCE.execute(json);
			CcpJsonRepresentation userIgnoredParameters = json.getJsonPiece(VisEntitySkillPending.Fields.email, VisEntitySkillPending.Fields.skill);
			String userIgnoredText = JbSupportSkillSuggestionMessages.userIgnored.getMessage(language, userIgnoredParameters);
			CcpJsonRepresentation jsonWithReply = JbSupportSkillSuggestionConversation.reply(json, userIgnoredText);
			CcpJsonRepresentation userIgnored = JbSupportSkillSuggestionStatus.userIgnored.throwException(jsonWithReply);
			return userIgnored;
		}

		if(answer.isNo()) {
			String options = JbSupportSkillSuggestionConversation.getOptions(language);
			CcpJsonRepresentation ignoreCanceledParameters = CcpOtherConstants.EMPTY_JSON.put(JbSupportSkillSuggestionMessageFields.options, options);
			String backToOptions = JbSupportSkillSuggestionMessages.ignoreCanceled.getMessage(language, ignoreCanceledParameters);
			CcpJsonRepresentation jsonWithReply = JbSupportSkillSuggestionConversation.reply(json, backToOptions);
			CcpJsonRepresentation ignoreCanceled = JbSupportSkillSuggestionStatus.ignoreCanceled.throwException(jsonWithReply);
			return ignoreCanceled;
		}

		String ignoreConfirmation = JbSupportSkillSuggestionConversation.getIgnoreConfirmation(json);
		CcpJsonRepresentation askAgainParameters = CcpOtherConstants.EMPTY_JSON.put(JbSupportSkillSuggestionMessageFields.ignoreConfirmation, ignoreConfirmation);
		String askAgain = JbSupportSkillSuggestionMessages.ignoreConfirmationNotUnderstood.getMessage(language, askAgainParameters);
		CcpJsonRepresentation jsonAskingAgain = JbSupportSkillSuggestionConversation.reply(json, askAgain);
		CcpJsonRepresentation askedAgain = JbSupportSkillSuggestionStatus.invalidAnswer.throwException(jsonAskingAgain);
		return askedAgain;
	}
}
