CONTROLLO DELLA SINTASSI SCxml

I controlli non vengono eseguiti su tutti gli elementi del file xml, ma solo su quegli elementi che seguono le specifiche sottostanti. Questo vuol dire che se esiste un figlio di <scxml> chiamato "Anghelli",
i nostri controlli ignoreranno lui e tutti i suoi discendenti, anche se questi fossero degli state delle transizioni ecc... Questo perche altrimenti non sarebbe possibile espandere il programma, e soprattutto 
perche altrimenti dovremmo controllare anche molti costrutti dell'scxml che chiaramente esulano dagli scopi del corso (controllo della correttezza sintattica di una chiamata al database di un server!).
Se un file scxml passa i nostri controlli noi assicuriamo le seguenti cose
1) <?xml>
	ATTRIBUTI
		C'� versione="1.0"


2) <scxml>

	ATTRIBUTI
		C'� xmlns="http://www.w3.org/2005/07/scxml".
		C'� version="1.0".
		C'� initial, ed � uguale ad uno degli stati.
	
	

	FIGLI
		C'� almeno uno tra state parallel e final.
		C'� al massimo un datamodel.


3) <data> (controlliamo solo i data figli di datamodel, a sua volta figlio di scxml)
	ATTRIBUTI
		C'� l'id ed � unico (tra i data ovviamente)
		C'� al massimo uno solo tra src e expr


4) <state> (controlliamo solo quelli che sono figli di state parallel o scxml)

	ATTRIBUTI
		C'� l'id ed � unico
		Se c'� initial, allora � un suo figlio diretto.


5) <parallel> (controlliamo solo quelli che sono figli di state parallel o scxml)
	
	ATTRIBUTI
		C'� l'id ed � unico


6) <final> (controlliamo solo quelli che sono figli di state parallel o scxml)
	
	ATTRIBUTI
		C'� l'id ed � unico


7) <transition> (controlliamo solo quelli che sono figli di state o parallel)

	ATTRIBUTI
		C'� almeno uno tra evento condizione e target
		Se c'� il target, allora � uno stato valido (cio� esiste), se non c'� si intende che � una transizione interna		


8) <assign>	(controlliamo solo quelli che sono figli di transition, onentry o onexit)	
	
	ATTRIBUTI
		ATTENZIONE:  a differenza di quanto scritto sul manuale scxml, l'attributo "location" � stato sostituito con "name".
		C'� name ed � valido, cio� esiste un data che ha come id quel name.
		












			