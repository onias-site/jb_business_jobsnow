package com.jb.business.bots.skill.suggestion;

import java.util.List;

import com.ccp.business.CcpBusiness;
import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntityCommandNotAllowedToUser;
import com.vis.entities.VisEntitySkillPending;

/**
 * First step of the {@code reviewSkillSuggestion} command ({@code /reviewSkillSuggestion <email> <skill>}): shows the
 * operator the skill suggested by the user, its synonyms and the justification the user gave ({@code description}),
 * and asks for the decision. The skill is the last parameter, so it may have spaces.
 *
 * <p>A user ignored by the support (the ignoring is global, {@link VisEntityCommandNotAllowedToUser}) is not reviewed, even if the operator
 * runs the command for them by mistake or on purpose: the flow is diverted with {@code userNotAllowed} before
 * anything is read. Without a pending suggestion (withdrawn by the user or already reviewed) the flow is diverted
 * with {@code requestNotFound}.
 */
public class JbSupportSkillSuggestionShowRequest implements CcpBusiness {

	/**
	 * Shows the suggestion.
	 * @param json the session, with {@code email} and {@code skill}
	 * @return the session with the reply
	 * @throws CcpErrorFlowDisturb with {@code userNotAllowed} or {@code requestNotFound}
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation json) {

		String email = json.getAsString(VisEntitySkillPending.Fields.email);
		CcpJsonRepresentation ignoredUser = CcpOtherConstants.EMPTY_JSON.put(VisEntityCommandNotAllowedToUser.Fields.email, email);
		boolean userIsIgnored = VisEntityCommandNotAllowedToUser.ENTITY.exists(ignoredUser);

		if(userIsIgnored) {
			JbSupportSkillSuggestionStatus.userNotAllowed.throwException(json);
		}

		CcpJsonRepresentation suggestionKey = json.getJsonPiece(VisEntitySkillPending.Fields.email, VisEntitySkillPending.Fields.skill);
		boolean suggestionIsNotPending = false == VisEntitySkillPending.ENTITY.exists(suggestionKey);

		if(suggestionIsNotPending) {
			JbSupportSkillSuggestionStatus.requestNotFound.throwException(json);
		}

		CcpJsonRepresentation storedSuggestion = VisEntitySkillPending.ENTITY.getOneById(suggestionKey);
		List<String> synonyms = storedSuggestion.getAsStringList(VisEntitySkillPending.Fields.synonym);
		String synonymNames = String.join(", ", synonyms);
		String description = storedSuggestion.getAsString(VisEntitySkillPending.Fields.description);

		JnLanguage language = JbSupportSkillSuggestionConversation.getLanguage(json);
		String options = JbSupportSkillSuggestionConversation.getOptions(language);
		CcpJsonRepresentation parameters = suggestionKey
				.put(VisEntitySkillPending.Fields.description, description)
				.put(JbSupportSkillSuggestionMessageFields.synonymNames, synonymNames)
				.put(JbSupportSkillSuggestionMessageFields.options, options);
		String request = JbSupportSkillSuggestionMessages.request.getMessage(language, parameters);

		CcpJsonRepresentation jsonWithReply = JbSupportSkillSuggestionConversation.reply(json, request);
		return jsonWithReply;
	}
}
