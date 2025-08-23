package shared;

/**
 * Definisce i comandi e le risposte del protocollo client-server.
 * Usato da entrambe le parti per mantenere coerenza nella comunicazione.
 */
public final class Protocol {

    //Comandi client → server
    public static final String CMD_LOGIN  = "LOGIN";   // LOGIN;email
    public static final String CMD_SEND   = "SEND";    // SEND;from;to;subject;body
    public static final String CMD_GET    = "GET";     // GET;user;lastId
    public static final String CMD_DELETE = "DELETE";  // DELETE;user;id

    //Risposte server → client
    public static final String RESP_OK    = "OK";      // operazione riuscita
    public static final String RESP_ERROR = "ERROR";   // errore generico

    /** Classe non istanziabile. */
    private Protocol() {}
}
