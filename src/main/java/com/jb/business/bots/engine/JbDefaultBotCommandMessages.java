package com.jb.business.bots.engine;

import com.jn.messages.JnSystemMessage;

/**
 * Texts sent by the commands every bot has ({@link JbDefaultBotCommandStep}), stored in {@code JnEntitySystemMessage}
 * and seeded by {@code JbEntityBot}. The placeholders of the templates are {@code commandName} and the fields of
 * {@link JbDefaultBotCommandMessageFields}.
 */
public enum JbDefaultBotCommandMessages implements JnSystemMessage {

	/** The operator left the command in progress: {@code {commandName}}, the name of the command left in the language. */
	commandExited,

	/**
	 * The language of the chat was set: {@code {languageName}}. A language is offered by {@code /setLanguage} only when
	 * this text was written in it.
	 */
	languageSet,

	/**
	 * {@code /setLanguage} typed without a language: the current one ({@code {languageName}}), how to type the command
	 * ({@code {commandName}}) and the languages that can be chosen ({@code {languages}}).
	 */
	chooseLanguage,

	/** {@code /setLanguage} typed with a language that cannot be chosen: {@code {typedLanguage}}, {@code {commandName}} and {@code {languages}}. */
	unknownLanguage,
	;
}
