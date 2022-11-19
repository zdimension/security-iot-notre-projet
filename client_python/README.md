# Partie client

Deux clients Python sont fournis :
- un utilitaire de signature + vérification
- un REPL, fournissant diverses commandes

## Utilitaire de signature `cipher.py`

Prend un nom de fichier en paramètre (supporte drag-n-drop).

Si le nom de fichier se termine par `.sign`, alors son contenu (la signature) est vérifiée par rapport au fichier correspondant sans l'extension.

Sinon, le fichier est hashé (SHA256), signé sur la carte, puis le résultat est écrit dans un fichier portant le même nom que le fichier d'origine, mais avec l'extension `.sign`.

Exemple : glisser le fichier `test.txt`, le fichier `test.txt.sign` sera créé.

![image](https://user-images.githubusercontent.com/4533568/202866562-255b93ab-926d-4436-972e-ad5d5950af82.png)

  
## REPL `repl.py`

REPL simple permettant de tester interactivement les fonctionnalités de l'applet. Une aide est disponible en tapant `help`.

![image](https://user-images.githubusercontent.com/4533568/202866699-c48f51c2-5b82-44e8-a040-a944a0abaad2.png)

### Hello World

On peut vérifier l'état des communications avec la carte avec `hello`, qui renvoie une chaîne stockée statiquement dans l'applet :

![image](https://user-images.githubusercontent.com/4533568/202866736-363ceb7f-c247-433d-80b1-43d8a1546bf2.png)

### Mode verbeux

Il est possible d'activer le mode verbeux pour afficher les détails des communications effectuées. Voici la même commande, avec le mode verbeux activé :

![image](https://user-images.githubusercontent.com/4533568/202866817-e616a2ef-b67f-40e7-b805-65afc95a2896.png)

On peut voir, dans l'ordre:
- le paquet d'instruction pour `HELLO`, encodé `42 01 00 00 00`
- le code de statut de la réponse, `6C 13`, fournissant la longueur de la réponse complète
- le second paquet, contenant la longueur à recevoir, `42 01 00 00 13`
- la seconde réponse, avec `90 00` (succès) et les 19 octets de réponse
- la réponse décodée par le REPL

### Authentification

On peut se connecter avec `login` :

![image](https://user-images.githubusercontent.com/4533568/202866993-54563669-fe9a-4d0b-985d-a7817a0f447d.png)

On peut tester de se déconnecter et d'entrer plusieurs fois un PIN incorrect :

![image](https://user-images.githubusercontent.com/4533568/202867019-952ae9d6-1ef7-4419-8d46-b8a1bdca6caf.png)

Après 3 essais, la carte est bloquée, et toute tentative de connexion est refusée, même avec le bon PIN. On peut alors réinitialiser l'applet :

![image](https://user-images.githubusercontent.com/4533568/202867064-79859b03-50c0-46e7-9e66-658e04d3f600.png)

Le PIN est remis à sa valeur par défaut (1234) et la paire de clés est régénérée.

### Modification du PIN

![image](https://user-images.githubusercontent.com/4533568/202867134-3b8ac2a9-c231-4158-98ac-ff93f485a606.png)

### Signature et vérification

On peut tester le système de signature :

![image](https://user-images.githubusercontent.com/4533568/202867237-1d203dd4-8c02-4b3e-8933-d1b75d475569.png)

![image](https://user-images.githubusercontent.com/4533568/202867326-7654df96-7b1f-4fa8-bd8d-45d1a52ca713.png)

(bien qu'il soit plus utile d'utiliser le script `cipher.py` conçu à cet effet)

### Gestion des clés

On peut afficher la clé publique (sans authentification) et la clé privée (en étant authentifié) :

![image](https://user-images.githubusercontent.com/4533568/202867210-4a3969fa-0254-484f-8666-b1ea190f6dfd.png)

On peut exporter ces deux clés vers une paire de fichiers `.pub` et `.pem` (respectivement la clé publique et la clé privée). Il est ainsi possible de vérifier côté client la signature, par exemple avec `openssl` :

```shell
$ openssl dgst -sha256 -verify key.pub -signature test.txt.sign test.txt
Verified OK
```
