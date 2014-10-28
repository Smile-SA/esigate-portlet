# Historique
ESI-Gate a été créé pour palier aux limites des iframes et répondre simplement au besoin d'intégration avec couplage faible.
Liferay bénéficierai grandement d'un portlet générique ESI-Gate pour agréger dans le portail une application métier tiers.

L'autre avantage est de permettre le développement des parties métiers sans devoir faire de dev Liferay


http://blog.smile.fr/Integrer-metier-et-liferay-avec-esi-gate-partie-1-2

http://blog.smile.fr/Integrer-metier-et-liferay-avec-esi-gate-partie-2-2


# Compilation de la portlet
Installer les artifacts maven fournit par liferay :
- Récupérer l'archive maven sur http://sourceforge.net/projects/lportal/files/Liferay%20Portal/
    (Fichier liferay-portal-maven-xxx.zip)
- Extraire l'archive       
- Executer la commande	"ant install"

Dans le projet esigate-portlet, lancer la commande suivante pour créer la portlet war :
	mvn package
La portlet est généré dans target/esigate-portlet.war


# Paramétrage

Configurer les providers esigate dans le fichier liferay/tomcat/lib/ext/esigate.properties :

	provider1.remoteUrlBase=http://localhost:4567/
	provider1.mappings=/*
	provider1.preserveHost=false

	providerMyApp.remoteUrlBase=http://myhost/myapp
	providerMyApp.mappings=/*
	providerMyApp.preserveHost=false


# Déploiement

Déployer la portlet esigate-portlet dans liferay 

Poser la portlet sur une page et aller dans la vue edit de la portlet pour la paramétrer 


# Fonctionnement/Limitations
La portlet utilise l'API esigate (proxy) pour intégrer le contenu d'applications. Ces applications (provider) sont préconfigurés dans le fichier esigate.properties.

Cette portlet permet d'intégrer un block par provider, si le block n'est pas définit, l'ensemble de la page provider sera intégré.

La portlet effectue une réécriture d'url du contenu fournit par le provider :
- les URL resources statics sont réécrites pour pointer sur la phase serveResource de la portlet,
- les actions des formulaires sont réécrites pour pointer sur la phase processAction de la portlet

Les URL construires par des fonctions javascript ne seront pas réécrites (ajax). Par contre, cela doit fonctionner si les URLs ajax sont définis dans la page html (a href ou form selon le besoin)

La porlet utilise le mécanisme de friendly URL pour la réécriture d'URL

L'authentification n'est pas géré dans le POC. Le mécanisme d'authentification standard d'Esigate peut-être utilisé (CAS par exemple).










