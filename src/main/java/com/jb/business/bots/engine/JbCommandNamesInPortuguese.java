package com.jb.business.bots.engine;

import com.ccp.decorators.CcpJsonFieldName;

/**
 * How each command of the bots is typed in Portuguese (without the {@code /}): the item is the canonical name of the
 * command and {@link #getValue()} its name in Portuguese. Seeded in {@code JbEntityBotCommandName}; the canonical name
 * stays valid in every language, and in English it is the name itself.
 */
public enum JbCommandNamesInPortuguese implements CcpJsonFieldName{
	/** {@code /solucionarTicketsDeTokenDeLogin}. */
	solveLoginTokenTicket { public String getValue() { return "solucionarTicketsDeTokenDeLogin"; } },
	/** {@code /ajustarHierarquiaDeHabilidades}. */
	fixSkillHierarchy { public String getValue() { return "ajustarHierarquiaDeHabilidades"; } },
	/** {@code /permitirComandoAoUsuario}. */
	allowCommandToUser { public String getValue() { return "permitirComandoAoUsuario"; } },
	/** {@code /ticketsPendentes}. */
	pendingTickets { public String getValue() { return "ticketsPendentes"; } },
	/** {@code /avaliarSugestaoDeHabilidade}. */
	reviewSkillSuggestion { public String getValue() { return "avaliarSugestaoDeHabilidade"; } },
	/** {@code /idDoChat}. */
	chatId { public String getValue() { return "idDoChat"; } },
	/** {@code /mostrarTodosOsComandos}. */
	showAllCommands { public String getValue() { return "mostrarTodosOsComandos"; } },
	/** {@code /explicarEsteBot}. */
	explainThisBot { public String getValue() { return "explicarEsteBot"; } },
	/** {@code /explicarEsteComando}. */
	explainThisCommand { public String getValue() { return "explicarEsteComando"; } },
	/** {@code /sair}. */
	exit { public String getValue() { return "sair"; } },
	/** {@code /definirIdioma}. */
	setLanguage { public String getValue() { return "definirIdioma"; } },
	;
}
