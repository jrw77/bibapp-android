Umlaute bei Ostfalia Wolfenbüttel:

Ostfalia Hochschule fuÌr angewandte Wissenschaften, Bibliothek
http://uri.gbv.de/organization/isil/DE-916?format=json liefert

Ostfalia Hochschule fuÌr angewandte Wissenschaften, Bibliothek


unicode                 utf-8
0000 0080 – 0000 07FF 	110xxxxx 10xxxxxx

Das ü ist im Unicode 00fc
in UTF-8 ist es  1100 0011 1011 1100 = c3 bc

hier ist es Ì :  c3 8c c2 88
welches UTF-8 code ist für   cc 88
das wiederum als utf-8 interpretiert wäre   11 0000 1000  0x308, :combining diaresis.
Zusammen mit dem u wäre das ein ü, sollte aber nicht so verwendet werden!

Lösungen: 1. Quickfix: Zeile DaiaLoader.java:115
          2. Korekte Lösung wäre eine Berichtigung des Datenbankeintrags. Der Fehler ist auch mit html sichtbar.
          http://uri.gbv.de/organization/isil/DE-916?format=html
          Koordinaten sich auch falsch, die anderen Standort fehlen.


Problem kommt von isil: in RDF und SRU ist es falsch, in JSON und html korrekt.

http://ld.zdb-services.de/data/organisations/DE-2310.rdf
