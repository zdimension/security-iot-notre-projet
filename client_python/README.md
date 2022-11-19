# Partie client

Deux clients Python sont fournis :
- un utilitaire de signature + vérification
- un REPL, fournissant diverses commandes

## Utilitaire de signature `cipher.py`

Prend un nom de fichier en paramètre (supporte drag-n-drop).

Si le nom de fichier se termine par `.sign`, alors son contenu (la signature) est vérifiée par rapport au fichier correspondant sans l'extension.

Sinon, le fichier est hashé (SHA256), signé sur la carte, puis le résultat est écrit dans un fichier portant le même nom que le fichier d'origine, mais avec l'extension `.sign`.

Exemple : glisser le fichier `test.txt`, le fichier `test.txt.sign` sera créé.
  
## REPL `repl.py`

REPL simple permettant de tester interactivement les fonctionnalités de l'applet. Une aide est disponible en tapant `help`.

Une commande particulière `export_keypair` permet d'exporter la paire de clés vers un fichier `.pub` et `.pem` (respectivement la clé publique et la clé privée). Il est ainsi possible de vérifier côté client la signature, par exemple avec `openssl` :

```shell
$ openssl dgst -sha256 -verify key.pub -signature test.txt.sign test.txt
Verified OK
```