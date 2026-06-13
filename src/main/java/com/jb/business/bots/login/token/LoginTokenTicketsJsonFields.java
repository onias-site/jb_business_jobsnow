package com.jb.business.bots.login.token;

import com.ccp.decorators.CcpJsonRepresentation.CcpJsonFieldName;

/**
 * Campos de controle usados no JSON de iteração sobre a lista de tickets de token de login:
 * {@code counter} (posição atual), {@code listValues} (lista), {@code listSize} (tamanho),
 * {@code position} (posição 1-based para exibição).
 */
enum LoginTokenTicketsJsonFields implements CcpJsonFieldName{
	counter, listValues, listSize, position
	;
}

enum OtherFields implements CcpJsonFieldName{
	alegation
}
