package com.jb.business.bots.skill.hierarchy;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;
import com.vis.business.skill.VisBusinessSkillFixHierarchyIgnoreUser;
import com.vis.entities.VisEntitySkillFixHierarchyPending;

/**
 * Step of the {@code fixSkillHierarchy} command that confirms the operator's intention to ignore the user.
 * {@code sim}/{@code yes} records the user in {@code VisEntityCommandNotAllowedToUser}, discards the request
 * without notifying the user and ends the session ({@code userIgnored}); {@code não}/{@code no} goes back to the
 * choice of how to decide the items ({@code ignoreCanceled}); any other answer asks again ({@code invalidAnswer}).
 */
public class JbSupportSkillFixHierarchyConfirmIgnore implements CcpBusiness {

	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		JbSupportSkillFixHierarchyAnswer answer = JbSupportSkillFixHierarchyAnswer.read(json);
		JnLanguage language = JbSupportSkillFixHierarchyConversation.getLanguage(json);
		boolean portuguese = JbSupportSkillFixHierarchyConversation.isPortuguese(language);

		if(answer.isYes()) {
			VisBusinessSkillFixHierarchyIgnoreUser.INSTANCE.execute(json);

			String email = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.email);
			String parent = json.getAsString(VisEntitySkillFixHierarchyPending.Fields.parent);
			String userIgnoredText = portuguese
					? "O usuário " + email + " foi ignorado no comando fixSkillHierarchy. A solicitação para o termo " + parent + " foi descartada."
					: "The user " + email + " was ignored in the fixSkillHierarchy command. The request for the term " + parent + " was discarded.";
			CcpJsonRepresentation jsonWithReply = json.put(JbSupportSkillFixHierarchyFields.botReply, userIgnoredText);
			CcpJsonRepresentation userIgnored = JbSupportSkillFixHierarchyStatus.userIgnored.throwException(jsonWithReply);
			return userIgnored;
		}

		if(answer.isNo()) {
			String options = JbSupportSkillFixHierarchyConversation.getOptions(language);
			String canceledText = portuguese ? "O usuário não será ignorado.\n\n" : "The user will not be ignored.\n\n";
			String backToOptions = canceledText + options;
			CcpJsonRepresentation jsonWithReply = json.put(JbSupportSkillFixHierarchyFields.botReply, backToOptions);
			CcpJsonRepresentation ignoreCanceled = JbSupportSkillFixHierarchyStatus.ignoreCanceled.throwException(jsonWithReply);
			return ignoreCanceled;
		}

		String notUnderstood = portuguese ? "Não entendi a resposta.\n\n" : "I did not understand the answer.\n\n";
		String ignoreConfirmation = JbSupportSkillFixHierarchyConversation.getIgnoreConfirmation(json);
		String askAgain = notUnderstood + ignoreConfirmation;
		CcpJsonRepresentation jsonAskingAgain = json.put(JbSupportSkillFixHierarchyFields.botReply, askAgain);
		CcpJsonRepresentation askedAgain = JbSupportSkillFixHierarchyStatus.invalidAnswer.throwException(jsonAskingAgain);
		return askedAgain;
	}
}
