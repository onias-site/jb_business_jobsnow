package com.jb.business.bots.engine;

import com.ccp.business.CcpBusiness;
import com.ccp.decorators.CcpJsonRepresentation;
import com.jn.json.fields.validation.JnJsonCommonsFields;
import com.jn.json.fields.validation.JnJsonInstantMessengerFields;

/**
 * Producer of the new session of {@link Bot}: the message received becomes the typed value, with the language of the
 * chat and, when there is no session, the command name. Its own input is ignored.
 */
class BotNewSessionProducer implements CcpBusiness {

	/** The message received. */
	private final CcpJsonRepresentation json;

	/** The language of the chat. */
	private final String chatLanguage;

	/**
	 * Keeps the message and the language.
	 * @param json the message received
	 * @param chatLanguage the language of the chat
	 */
	BotNewSessionProducer(CcpJsonRepresentation json, String chatLanguage) {
		this.json = json;
		this.chatLanguage = chatLanguage;
	}

	/**
	 * Produces the new session.
	 * @param ignored the input, not used
	 * @return the new session
	 */
	public CcpJsonRepresentation apply(CcpJsonRepresentation ignored) {
		CcpJsonRepresentation newSession = this.json
		.mergeWithAnotherJson(this.json)
		.renameField(JnJsonInstantMessengerFields.message, JnJsonCommonsFields.typedValue)
		.put(JnJsonCommonsFields.language, this.chatLanguage)
		.getTransformedJson(JsonProducers.putCommandNameWhenHasNoSession)
		;
		return newSession;
	}
}
