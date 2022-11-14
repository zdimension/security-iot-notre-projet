mode_201
enable_trace
enable_timer
establish_context
card_connect
select -AID A000000018434D00
open_sc -security 3 -keyind 0 -keyver 0 -key 47454d5850524553534f53414d504c45 -keyDerivation visa2
delete -AID a0404142434445461001
delete -AID a04041424344454610
install -file out/notreprojet/javacard/notreprojet.cap -sdAID A000000018434D00 -nvCodeLimit 4000
card_disconnect
release_context