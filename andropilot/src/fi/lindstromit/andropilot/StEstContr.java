// 
// Copyright (C) 2012 Jukka Ranta
//
package fi.lindstromit.andropilot;

public class StEstContr {

	
	StEstContr(float deltaTin, int umaxin, float tp, float tv){
		deltaT = deltaTin; // ohjauksen päivitysväli sekunteina
		umax = umaxin; // ohjaussignaalin maksimiarvo

		// -- Veneen dynamiikka --
		// pinna saadaan keskeltä maksimikulmaansa ohjaussignaalin maksimiarvolla
		// float tp sekunnissa
		// maksimiarvo johon pinna kääntyy (ohjaussignaalista skaalaamaton kulma)
		x2max = umax*tp/deltaT;
		// vene tekee piruetin kun pinna autopilotin maksimiarvossa
		// float tv sekunnissa (nopeudella 4 solmua)
		// parametri pinnasta veneen suuntakulmaan
		a1=-360*deltaT/tv/(4*x2max);
		
		// alkuarvot
		p1=100;
		p2=x2max*x2max/10f;
		p3=0;
		x1=0;
		x2=0;
		um=0;
		yp=0;
		// parametrit
		q1=2;
		q2=x2max*x2max/50f;
		r=2;

		g1=15f;
//		g1=-0.01f/a1;
		g2=-0.15f*2500f/x2max;
	}

	// ohjauksen päivitysväli sekunneissa
	float deltaT;
	
	// parametreista:
	// pinna keskeltä ääriasentoon tp sekunnissa
	// vene ympäri (360 deg) tv sekunnissa nopeudella V
	// algoritmin päivitystiheys deltaT
	// ohajussignaalin maksimiarvo umax
	// max pinnakulma (skaalaamaton luku tilamuuttuja x2) x2max = umax*tp/deltaT
	// 
	// a1 = 360*deltaT/(tv*x2max*V)
	
	// globaalit muuttujat

	// tilan estimaatti: x1=suuntapoikkeama, x2=peräsinkulma
	// x1 = suuntapoikkeama asteina
	// x2 = peräsinkulma epämääräisesti skaalattuna (sisäinen
	// ... muuttuja, skaala ei olennainen kunhan vastaa muita parametreja)
	float x1, x2;

	// tilaestimaatin kovarianssimatriisi
	// |p1 p3|
	// |p3 p2|
	float p1, p2, p3;
	// alkuarvot vaikkapa p1=100, p2=100, p3=0

	// ojaussignaalin maksimiarvo
	int umax;
	// peräsinkulman maksimiarvo
	float x2max;
	
	// edellisen ohjaussignaalin muisti
	int um;

	// edellisen suuntavirheen muisti
	// (huomioidaan jos heittää ympäri +-180
	float yp=0;
	
	// Vakioita
	// tilojen ja mittausten varianssit
	// q1=suuntakulman, q2=peräsinkulman muutos aika-askeleen aikana
	// r=mittausvirheen varianssi
	float q1, q2, r;
	// arvot vaikkapa q1=1, q2=1, r=1

	// malli ja sen parametri (peräsinkulman kerroin integroitaessa suuntakulmaksi)
	// x1(t+dt) = x1(t) + a1*v*x2(t)
	// x2(t+dt) = x2(t) + u(t)
	// v=veneen nopeus (mittaus), u=ohjaus, a1=parametri
	float a1; // arvo riippuu algoritmin suoritustaajuudesta
	// ... a1<0 koska peräsin vääntää venettä vastakkaiseen
	// ... suuntaan (pinna oikealle->vene vasemmalle)

	// ohjausalgoritmin kerroin suuntavirheelle
	float g1;
	// ohjausalgoritmin kerroin peräsinkulmalle
	float g2;

	/**
	 * 
	 * @param y suuntapoikkeama: mitattu-tavoite
	 * @param v paatin nopeus solmuina
	 * @param newY tuliko uusi mittaus (true) vai päivitetäänkö ohjaus vanhalla datalla (false)
	 * @param trgtHdChanged onko tavoitesuunta muuttunut edellisen mittauksen jälkeen (eli uusin
	 * mittaus ei ole ole järkevää dataa)
	 * @return
	 */
	int headingControl(float y, float v,
			boolean newY, boolean trgtHdChanged){
		
		if(newY){
			// jos vetäisty ympäri epäjatkuvudesta +-180 asteen kohdalla
			// korjataan vanha estimaatti oikein päin
			if(yp<-160 && y>160) x1 = 360+yp;
			else if(yp>160 && y<-160) x1 = -360+yp;
			// kulma muistiin seuraavaa kierrosta varten
			yp=y;
		}

		// veneen nopeuden huomioiva mallin parametri
		float b = a1 * v;

		// tilan estimointi
		// ennuste tilalle ennen mittausta
		float x1p = x1 + b * x2 + b/2*um;
		// jos tavoitesuunta muuttunut, ei korjailla
		// ennustetta suodattamalla vaan ronskisti alustetaan uudella virheellä
		if(trgtHdChanged){
			x1p = y;
		}
		float x2p = x2 + um;
		if(x2p>x2max*1.5f) x2p=x2max*1.5f; // ettei estimaatti karkaa ihan mahdottomiin arvoihin
		if(x2p<-x2max*1.5f) x2p=-x2max*1.5f;
		// ennuste P:lle ennen mittausta
		float p1p = p1 + b*(2*p3+b*p2) + q1;
		float p2p = p2 + q2;
		float p3p = p3 + b*p2;
		// ennustevirhe
		float yv = y - x1p;
		// Kalman vahvistuskertoimen jakaja s
		float s = p1p + r;
		// Kalman vahvistus
		float k1 = p1p/s;
		float k2 = p3p/s;
		// jos ei ollutkaan uutta dataa
		if(!newY){
			k1=0f;
			k2=0f;
		}
		// päivitetty tilan ennuste
		x1 = x1p + k1*yv;
		x2 = x2p + k2*yv;
		// tilaestimaatin kovarianssimatriisin päivitys
		p1 = p1p*(1-k1);
		p2 = p2p-k2*p3p;
		p3 = p3p*(1-k1);

		// ohjaus (lineaarinen takaisinkytkentä estimoidusta tilasta)
		// pyrkii nollaamaan suuntavirheen (g1*x1) ja
		// ... peräsinkulman (g2*x2)
		// tarkasta parametrien etumerkit vastaamaan pinnan ohjausmoottorin suuntaa !!!
		// jos suuntakulma mitataan myötäpäivään (normaali kompassi) ja
		// ... positiivinen u kääntää pinnaa oikealle (-> vene kaartaa vasemmalle)
		// ... niin g1 positiivinen ja
		// ... g2 negatiivinen
		int u = (int) (((10f/(2+v))*g1*x1 + (1+v/10f)*g2*x2));
//		if(x1*x2<0) u=(int)(1.5f*u); // kovempi ohjaus jos peräsin väärään suuntaan
		if(u<-umax) u=-umax;
		if(u>umax) u=umax;

		// laitetaan ohjaussignaali muistiin algoritmin seuraavaa kierrosta varten
		um=u;
		return u;
	}
}