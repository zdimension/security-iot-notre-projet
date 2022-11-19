# Partie carte

## Outils requis

- JDK 8 (disponible [par exemple ici](https://sdlc-esd.oracle.com/ESD6/JSCDL/jdk/8u341-b10/424b9da4b48848379167015dcc250d8d/jdk-8u341-windows-x64.exe?GroupName=JSC&FilePath=/ESD6/JSCDL/jdk/8u341-b10/424b9da4b48848379167015dcc250d8d/jdk-8u341-windows-x64.exe&BHost=javadl.sun.com&File=jdk-8u341-windows-x64.exe&AuthParam=1668616581_6bbc106434d1bee4e75213cc112363e0&ext=.exe) – lien vers les serveurs d'Oracle susceptible de disparaître, s'il ne fonctionne plus, chercher sur Google un lien à jour)
- IntelliJ (à défaut, de quoi compiler un projet Ant)
- Pour Windows, gpshell (disponible [ici](https://freefr.dl.sourceforge.net/project/globalplatform/GPShell/GPShell-2.2.0/gpshell-binary-2.2.0.zip))
  - Télécharger le zip, et l'extraire dans le dossier `card`, de sorte à ce le fichier `gpshell.exe` se trouve dans le même dossier que le `README.md` que vous êtes en train de lire
- Pour Linux, le paquet gpshell correspondant à votre distribution

## Installation de l'applet

Compiler le projet via la cible Ant `binarize.all.standard` (ça vous donnera un fichier `out/notreprojet/javacard/notreprojet.cap`).
- dans IntelliJ, voir le panneau latéral "Ant" :

![image](https://user-images.githubusercontent.com/4533568/202867541-a730e7c0-a1f8-4018-b67d-c09910dd47f2.png)

Ensuite, lancer `gpshell upload.gp` dans le dossier `card`. 

![6a0120a85dcdae970b0128776ff992970c-pi (3)](https://user-images.githubusercontent.com/4533568/202867490-b0bcfb5f-df2d-4426-af53-0fc0172446cc.png)

L'applet est maintenant sur la carte, et les [clients Python](../client_python) sont maintenant utilisables.
