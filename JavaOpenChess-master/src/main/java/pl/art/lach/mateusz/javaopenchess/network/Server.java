package pl.art.lach.mateusz.javaopenchess.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Class responsible for server references: For running the server,
 * settings of the players, for clients references on the server
 * and references of observators
 * 
 * @author Mateusz  Lach ( matlak, msl )
 * @author Damian Marciniak
 */
public class Server implements Runnable
{
    private static final Logger LOG = Logger.getLogger(Server.class);
    
    public static boolean isPrintEnable = true; //print all messages (print function)

    private static Map<Integer, Table> tables;
    
    public static int port = 4449;
    
    private static ServerSocket ss;
    
    private static boolean isRunning = false;

    public Server()
    {
        if(!Server.isRunning) //run server if isn't running previous
        {
            runServer();

            Thread thread = new Thread(this);
            thread.start();

            Server.isRunning = true;
        }
    }

    /*
     * Method with is checking is the server is running
     * @return bool true if server is running, else false
     */
    public static boolean isRunning()
    {
        return isRunning;
    }

    /*
     * Method to starting a new server
     * It's running a new game server
     */

    private static void runServer()
    {
        try
        {
            ss = new ServerSocket(port);
            print("running");
        }
        catch (IOException ex)
        {
            LOG.error("runServer/IOException: ", ex);
        }

        tables = new HashMap<>();
    }

    @Override
    public void run() //listening
    {
        print("listening port: "+port);
        while(true)
        {
            Socket s;
            ObjectInputStream input;
            ObjectOutputStream output;

            try
            {
                s = ss.accept();
                input = new ObjectInputStream(s.getInputStream());
                output = new ObjectOutputStream(s.getOutputStream());

                print("new connection");

                //readed all data
                int tableID = input.readInt();
                print("readed table ID: "+tableID);
                boolean joinAsPlayer = input.readBoolean();
                print("readed joinAsPlayer: "+joinAsPlayer);
                String nick = input.readUTF();
                print("readed nick: "+nick);
                String password = input.readUTF();
                print("readed password: "+password);
                //---------------

                if(!tables.containsKey(tableID))
                {
                    print("invalid table ID");
                    output.writeInt(ConnectionInformation.ERROR_INVALID_TABLE_ID.getValue());
                    output.flush();
                    continue;
                }
                Table table = tables.get(tableID);

                if(!table.getPassword().equals(password))
                {
                    print("invalid password");
                    output.writeInt(ConnectionInformation.ERROR_INVALID_PASSWORD.getValue());
                    output.flush();
                    continue;
                }
                 
                if(joinAsPlayer)
                {
                    print("join as player");
                    if(table.isAllPlayers())
                    {
                        print("error: was all players at this table");
                        output.writeInt(ConnectionInformation.ERROR_TABLE_IS_FULL.getValue());
                        output.flush();
                    }
                    else
                    {
                        print("wasn't all players at this table");

                        output.writeInt(ConnectionInformation.EVERYTHING_OK.getValue());
                        output.flush();

                        table.addPlayer(new ServerClient(input, output, nick, table));
                        table.sendMessageToAll(String.format("** Player %s has joined the game **", nick));

                        if(table.isAllPlayers())
                        {
                            table.generateSettings();

                            print("Send settings to all");
                            table.sendSettingsToAll();

                            table.sendMessageToAll(
                                String.format("** New game, player %s starts", table.getClientPlayerWhite().getNick())
                            );
                        }
                        else
                        {
                            table.sendMessageToAll("** Awaiting for second player **");
                        }
                    }
                }
                else//join as observer
                {
                    print("join as observer");
                    if(!table.canObserversJoin())
                    {
                        print("Observers can't join");
                        output.writeInt(ConnectionInformation.ERROR_OBSERVERS_NOT_ALLOWED.getValue());
                        output.flush();
                    }
                    else
                    {
                        output.writeInt(ConnectionInformation.EVERYTHING_OK.getValue());
                        output.flush();

                        table.addObserver(new ServerClient(input, output, nick, table));

                        if(table.getClientPlayerBlack() != null) //all players is playing
                        {
                            table.sendSettingsAndMovesToNewObserver();
                        }

                        table.sendMessageToAll(String.format(
                            "** Observer %s has joined the game **", nick
                        ));
                    }
                }
            }
            catch (IOException ex)
            {
                LOG.error("runServer/IOException: " + ex);
            }
        }
    }

    /*
     * Method with is printing the servers message
     *
     */
    public static void print(String str)
    {
        if(isPrintEnable)
        {
            LOG.debug("Server: "+str);
        }
    }

    /*
     * Method with is creating a new table
     * @param idTable int witch number of the table
     * @param password String with password
     * @param withObserver bool true if observers available, else false
     * @param enableChat bool true if chat enable, else false
     */
    public void newTable(int idTable, String password, boolean withObserver, boolean enableChat) //create new table
    {
        print("create new table - id: " + idTable);
        tables.put(idTable, new Table(password, withObserver, enableChat));
    }

    /*
     * Method with sets a players settings and pawns
     */



}
