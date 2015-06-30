package org.irmacard.web.restapi.util;

import java.util.List;
import java.util.Map;

import net.sourceforge.scuba.smartcards.ProtocolCommand;
import net.sourceforge.scuba.smartcards.ProtocolCommands;

/**
 * Data structure for communicating protocol steps.
 * @author Maarten Everts (TNO)
 *
 */
public class ProtocolStep {
	public ProtocolInfo info;
	public String status;
	public ProtocolCommands commands; // soon to be deprecated
    public Map<Short,List<ProtocolCommand>> commandsSets;
    
    public String responseurl;

    public boolean protocolDone = false;

    public boolean usePIN = false;

    public boolean askConfirmation = false;
    public String confirmationMessage;
    
    public String feedbackMessage;
    
    public String result = null;
	
    public static ProtocolStep newError(String errorMessage) {
    	ProtocolStep ps = new ProtocolStep();
		ps.feedbackMessage = errorMessage;
		ps.status = "error";
		ps.protocolDone = true;
		return ps;
    }
}
