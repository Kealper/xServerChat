<?php

namespace xServerPM;

use pocketmine\scheduler\PluginTask;

class Tasks extends PluginTask {

	public function onRun(int $currentTick) {
		$this->getOwner()->checkClient();
	}

}