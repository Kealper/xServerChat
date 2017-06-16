<?php

namespace xServerPM;

use pocketmine\scheduler\PluginTask;

class Tasks extends PluginTask {
	
	public function onRun($currentTick) {
		$this->getOwner()->checkClient();
	}
	
}