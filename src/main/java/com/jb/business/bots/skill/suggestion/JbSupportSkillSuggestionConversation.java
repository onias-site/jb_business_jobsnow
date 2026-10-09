package com.jb.business.bots.skill.suggestion;

import com.ccp.constants.CcpOtherConstants;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jb.business.bots.skill.hierarchy.JbSupportSkillFixHierarchyFields;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.utils.JnLanguage;
import com.vis.entities.VisEntitySkillPending;

/**
 * Texts sent to the support bot operator along the {@code reviewSkillSuggestion} command, in the language of the
 * session. The texts themselves live in {@link JbSupportSkillSuggestionMessages}.
 */
final class JbSupportSkillSuggestionConversation {

	/** Utility class; not instantiable. */
	private JbSupportSkillSuggestionConversation() {}

	/**
	 * Returns the language of the session (the enum itself or its name), Portuguese by default.
	 * @param json the session
	 * @return the language
	 */
	static JnLanguage getLanguage(CcpJsonRepresentation json) {
		String languageName = json.getAsString(JnJsonCommonsFields.language);
		boolean noLanguage = languageName.isEmpty();
		JnLanguage language = noLanguage ? JnLanguage.portuguese : JnLanguage.valueOf(languageName);
		return language;
	}

	/**
	 * Returns the options offered to the operator.
	 * @param language the language
	 * @return the options
	 */
	static String getOptions(JnLanguage language) {
		String options = JbSupportSkillSuggestionMessages.options.getMessage(language);
		return options;
	}

	/**
	 * Asks the operator to confirm that the user will be ignored for the command.
	 * @param json the session
	 * @return the question
	 */
	static String getIgnoreConfirmation(CcpJsonRepresentation json) {
		JnLanguage language = getLanguage(json);
		CcpJsonRepresentation parameters = json.getJsonPiece(VisEntitySkillPending.Fields.email);
		String ignoreConfirmation = JbSupportSkillSuggestionMessages.ignoreConfirmation.getMessage(language, parameters);
		return ignoreConfirmation;
	}

	/**
	 * Tells that the answer was not understood and asks for the options again.
	 * @param json the session
	 * @return the text
	 */
	static String getNotUnderstood(CcpJsonRepresentation json) {
		JnLanguage language = getLanguage(json);
		String options = getOptions(language);
		CcpJsonRepresentation parameters = CcpOtherConstants.EMPTY_JSON.put(JbSupportSkillSuggestionMessageFields.options, options);
		String notUnderstood = JbSupportSkillSuggestionMessages.notUnderstood.getMessage(language, parameters);
		return notUnderstood;
	}

	/**
	 * Puts the text to send to the operator in {@code botReply}.
	 * @param json the session
	 * @param reply the text
	 * @return the session with the reply
	 */
	static CcpJsonRepresentation reply(CcpJsonRepresentation json, String reply) {
		CcpJsonRepresentation jsonWithReply = json.put(JbSupportSkillFixHierarchyFields.botReply, reply);
		return jsonWithReply;
	}
}
