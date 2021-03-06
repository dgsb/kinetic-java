/**
 * 
 * Copyright (C) 2014 Seagate Technology.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.seagate.kinetic.example.openstorage;

import java.io.File;

import kinetic.simulator.KineticSimulator;
import kinetic.simulator.SimulatorConfiguration;

/**
 * Starts (by default) 1000 simulators each listens on its own ports and with its own persistent storage.
 * <p>
 * You may require to configure the OS open process/file parameters to run this program.
 * 
 * @see OpenStorageClient
 * @author chiaming
 */
public class VirtualDrives {
    
    public static final int MAX_SIMULATOR = 100;

    private int maxSimulator = MAX_SIMULATOR;
    
 // base port number
    private int port = 8123;

    // base ssl port
    private int sslPort = 18123;
    
    public VirtualDrives(int maxSimulator, int portBase, int sslportBase) {
        this.maxSimulator = maxSimulator;

        this.port = portBase;

        this.sslPort = sslportBase;
    }
    
    public void run() {
        
     // simulator instances holder
        KineticSimulator simulators[] = new KineticSimulator[maxSimulator];

        for (int i = 0; i < maxSimulator; i++) {

            // instantiate a new instance of configuration object
            SimulatorConfiguration config = new SimulatorConfiguration();
            //config.setStartSsl(false);
            //config.setUseMemoryStore(true);

            // set service ports to the configuration
            int myport = port + i;
            int mySslPort = sslPort + i;
            config.setPort(myport);
            config.setSslPort(mySslPort);

            // set kinetic home for each drive
            String kineticHome = System.getProperty("user.home")
                    + File.separator + "kinetic" + File.separator + "instance_"
                    + myport;

            // set persist store home folder for each instance
            config.put(SimulatorConfiguration.KINETIC_HOME, kineticHome);

            // start the simulator instance
            simulators[i] = new KineticSimulator(config);

            System.out.println("\n " + i + ": started simulator. port="
                    + config.getPort() + ", ssl port=" + config.getSslPort()
                    + "\n");
        }
        
    }
    
	public static void main(String[] args) throws InterruptedException {
	    
	    //use bdb store 
	    //System.setProperty("kinetic.db.class", "com.seagate.kinetic.simulator.persist.bdb.BdbStore");
        int maxSimulator = 10;
        if (args.length >= 1) {
            maxSimulator = Integer.parseInt(args[0]);
        }

        int portBase = 8123;
        if (args.length >= 2) {
            portBase = Integer.parseInt(args[1]);
        }

        int sslportBase = 18123;
        if (args.length >= 3) {
            sslportBase = Integer.parseInt(args[2]);
        }
	    
		// max number of simulators to instantiate.
        VirtualDrives vdrives = new VirtualDrives(maxSimulator, portBase,
                sslportBase);
		//start the simulator
		vdrives.run();
	}


}
