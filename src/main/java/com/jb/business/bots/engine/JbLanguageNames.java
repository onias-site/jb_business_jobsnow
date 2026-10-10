package com.jb.business.bots.engine;

import com.jn.messages.JnSystemMessage;

/**
 * The names of the languages shown to the operator of a bot by {@code /setLanguage}, one item per {@code JnLanguage},
 * stored in {@code JnEntitySystemMessage} and seeded by {@code JbEntityBot}: the text of an item in a language is the name
 * of the language of the item written in that language.
 */
public enum JbLanguageNames implements JnSystemMessage {

	/** The name of Portuguese. */
	portuguese,

	/** The name of English. */
	english,

	/** The name of Spanish. */
	spanish,
	;
}
