import inspect
import re
import sys
import traceback

from colorama import init

from internal.card import init_card, Response, Logger
from internal.consts import *

init()

if "-v" in sys.argv:
    Logger.log_verbose = True

card = init_card()

print()

commands = card.commands()
auth_marker = RED("#")
for name, func in commands.items():
    args = YELLOW(
        " ".join(f"<{x}>" for x in inspect.signature(func).parameters).ljust(26 - len(name)))
    func.__func__.help = " ".join([
        auth_marker if func.requires_auth else " ",
        name, args,
        # highlight argument names
        re.sub(rf"({'|'.join(inspect.signature(func).parameters)})", YELLOW("\\1"), func.__doc__,
               flags=re.IGNORECASE)
    ])

help_msg = f"Type '{YELLOW('help')}' for a list of available commands"
print("Card REPL active.", help_msg)
while True:
    print(CYAN("> "), end="")
    command = input().strip()
    if not command:
        continue
    cmd, *args = command.split()
    if cmd == "exit":
        break
    elif cmd == "verbose":
        Logger.log_verbose = not Logger.log_verbose
        print("Verbose mode", GREEN('enabled') if Logger.log_verbose else RED('disabled'))
    elif cmd == "help":
        print("Commands:")
        for func in commands.values():
            print(func.help)
        print()
        print(f"Commands marked with a {auth_marker} require authentication.")
        print(
            "You can call a command by typing a prefix of its name, e.g. 'h' for 'hello'.")
    elif fct := [*(fct for name, fct in commands.items() if name.startswith(cmd)), None][0]:  # prefix lookup
        try:
            resp = fct(*args)
            if resp is None:
                resp = GREEN("Success")
            else:
                resp = resp.processed if isinstance(resp, Response) else resp
            print(GREEN("="), resp)
        except Response as r:
            print(RED(f"! Card returned non-OK status: {r.status}"))
            if r.status.sw == SW_SECURITY_STATUS_NOT_SATISFIED:
                print(YELLOW("?"), "Try authenticating first using", YELLOW("login"))
        except Exception as e:
            print(RED(f"! {e}"))
            if Logger.log_verbose:
                traceback.print_tb(e.__traceback__)
            print(YELLOW("?"), "Usage:")
            print(fct.help)
    else:
        print(RED("! Unknown command '" + RESET(cmd) + RED("'")))
        print(YELLOW("?"), help_msg)
