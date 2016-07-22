CONTROLLO DELLA SINTASSI SCxml

I controlli non vengono eseguiti su tutti gli elementi del file xml, ma solo su quegli elementi che seguono le specifiche sottostanti.
 Questo vuol dire che se esiste un figlio di <scxml> chiamato "Anghelli", i nostri controlli ignoreranno lui e tutti i suoi discendenti, 
anche se questi fossero degli state delle transizioni ecc...
 Questo perche altrimenti non sarebbe possibile espandere il programma, e soprattutto perche altrimenti dovremmo controllare anche molti costrutti dell'scxml 
che chiaramente esulano dagli scopi del corso (controllo della correttezza sintattica di una chiamata al database di un server!).


Se un file scxml passa i nostri controlli noi assicuriamo le seguenti cose


1) <?xml>


	ATTRIBUTI

		C'� versione="1.0        OK   


2) <scxml>


	ATTRIBUTI
		
                C'� xmlns="http://www.w3.org/2005/07/scxml".   OK        
   		C'� version="1.0".
                             OK
		C'� initial, ed � uguale ad uno degli stati.
   OK
                SE initial manca                               OK
	FIGLI
		
        C'� almeno uno tra state parallel e final.
    OK
        C'� al massimo un datamodel.


     OK


3) <data> (controlliamo solo i data figli di datamodel, a sua volta figlio di scxml)


	ATTRIBUTI

		C'� l'id ed � unico (tra i data ovviamente)
  OK
		C'� al massimo uno solo tra src e expr  
   


4) <state> (controlliamo solo quelli che sono figli di state parallel o scxml)


	ATTRIBUTI

		C'� l'id ed � unico       OK
		
                Se c'� initial, allora � un suo figlio diretto.


    

5) <parallel> (controlliamo solo quelli che sono figli di state parallel o scxml)
 
	
ATTRIBUTI

		C'� l'id ed � unico


 

6) <transition> (controlliamo solo quelli che sono figli di state o parallel)


	ATTRIBUTI

		C'� almeno uno tra evento condizione e target         	OK
 		Se c'� il target, allora � uno stato valido (cio� esiste), se non c'� si intende che � una transizione interna   OK
		



7) <assign>	(controlliamo solo quelli che sono figli di transition, onentry o onexit)
       ATTRIBUTI
             il data esiste  OK
		
            
  ATTENZIONE:  a differenza di quanto scritto sul manuale scxml, l'attributo "location" � stato sostituito con "name".

		C'� name ed � valido, cio� esiste un data che ha come id quel name.
        OK
		

               

file con errori

versionedata
               versione : 2.ta    
targeterrati
	        doppio -> pippo (non esiste)
dataerrati
		        pippo (non esiste)
assignlocation
		    totale e contatore non hanno attributo name
datamodelinitial	
          	datamodel doppio  initial_state<pippo
eventcondtarget
			non c'e' evento cond e target 
xmlnsinitial            
                xmlns/="http://www.w3.org/2005/07/scxml" manca initial
statistessonome
		    2 volte lo stato standby con lo stesso nome





			