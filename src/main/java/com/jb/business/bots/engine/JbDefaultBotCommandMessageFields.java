package com.jb.business.bots.engine;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * Placeholders of the texts of {@link JbDefaultBotCommandMessages}: the name of a language ({@code languageName}), the
 * names of the languages that can be chosen ({@code languages}) and the language the operator typed
 * ({@code typedLanguage}).
 */
public enum JbDefaultBotCommandMessageFields implements CcpJsonFieldName{
	/** The {@code languageName} field. */
	languageName,
	/** The {@code languages} field. */
	languages,
	/** The {@code typedLanguage} field. */
	typedLanguage
}
