Before Use:
Be aware that this plugin will try to load every world that contains a player-set-home (inside the config.yml of the plugins data folder).

Usage:
Simply drop the jar File of this plugin into the plugins directory of your server. No specific setup required.

commands:
setHome:
This saves the current command sender's (must be a player) location as a home in the config.yml file of the plugin.
The player is then able to teleport to that location (via /home).
usage: "/setHome <name of home(string)>"

home:
This teleports the command sender (must be a player) to a home.
Another player can be invited by providing the other player's name as the 2nd argument.
usage: "/home <name of home(string)> <player to invite (optional)>"

setAllowInvitations:
Disables other players from inviting this command sender (must be a player) to a home.
usage: "/blockHomeInvitations <Boolean>"

acceptHomeInvitation:
Accepts an invitation to another player's home.
alias: "/ahi"

setMaxHomes:
Sets the maximum amount of homes a player can set: -1 -> no homes; 0 -> no restricton
permission: op
usage: "/setMaxHomes <integer>"

deleteHome:
Deletes the specified home. (Frequent usage not recommended)
usage: "/deleteHome <name of home(string)>"

homes:
Shows you all the homes you have created. If you provide a player's name (as you are a op) the command will show you their homes.
usage: "/homes"
