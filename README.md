# Projet de sécurité / IoT

Documentation des composants :

## [Applet JavaCard](card)

## [Clients Python](client_python)

## Rapport de développement

Nous sommes partis de l'exemple Hello World fourni, et avons commencé par mettre en place une architecture de code permettant de gérer plusieurs instructions (décodage du champ `INS`, appel de la fonction correspondante). De là, nous avons pu implémenter la gestion du code PIN et de l'authentification ; faute d'une documentation correcte pour `OwnerPIN`, nous avons pu nous inspirer de [ce projet](https://github.com/Toporin/SatochipApplet/blob/master/src/org/satochip/applet/CardEdge.java). Concrètement, un objet `OwnerPIN` est stocké de manière persistante dans l'applet et fournit les fonctionnalités usuelles d'un PIN (vérifier, gérer le nombre d'essais restant avant blocage, modifier).

Pour le chiffrement, nous avons pu utiliser les classes du package `javacard.security` qui correspond plus ou moins aux classes disponibles sur bureau dans `javax.crypto`. Un objet `KeyPair` est instancié lors de la [ré]initialisation de l'applet, et est stocké de manière persistante. Cependant, ce package ne supporte pas le hachage SHA-256, celui-ci est donc effectué côté client (ce qui ne change rien à la validité du processus). Côté carte, la charge à signer est construite à partir du hash transmis et de l'identifiant ASN.1 pour le SHA-256, et le tout est chiffré avec la clé privée pour aboutir à une signature.

Le client a été développé en tandem avec l'applet, et les tests se faisaient ainsi via l'envoi des APDUs à tester depuis le REPL du client. L'utilitaire `openssl` sur ordinateur a été utilisé pour s'assurer que les signatures étaient correctement générées et valides (en effet, on peut vérifier avec `openssl` une signature générée par la carte, et ça fonctionne).
