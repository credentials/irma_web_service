package org.irmacard.web.restapi.util;

import java.util.List;

import net.sourceforge.scuba.smartcards.ProtocolCommand;

public class ProtocolStep {
    public List<ProtocolCommand> commands;
    public String responseurl;
    public boolean usePIN;
    public String feedbackMessage;
    public String confirmationMessage;
    public boolean askConfirmation = false;
    public boolean protocolDone = false;
    public String result = null;
    public String data = null; 
}
