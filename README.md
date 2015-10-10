Three Phase Commit

CS380D Distributed Computing I — Project 1

Name : Tian Zhang  
ID: tz3272

Name: Bradley Beth
ID: bethbg

====================================================================================
Mistake in reading

The mistake is we do need to log the precommit.


Without logging precommit :
   1. Coordinator writes Commit to its logs after getting ACKs from everyone, 
   but dies before sending commits to anyone.
   
   2. Now the Coordinator dies. All the participants timeout, then run the 
   election protocol electing the operational process with the lowest PID as 
   the new coordinator. The state of all participants is "COMMITABLE". 

   3. At this point, all the participants die, thereby inducing total failure. 
   Now all the participants start recovering except the coordinator. Since no 
   precommit was logged, they see their votes and move to the state "UNCERTAIN". 
   
   4. Everyone is in uncertain state so the decision taken is abort.
   
   5. When the coordinator wakes up, there is an inconsistency in the 
   decision taken by the coordinator and the decision taken by the participants. 


 
