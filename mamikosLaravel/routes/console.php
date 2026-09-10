<?php

use Illuminate\Foundation\Inspiring;
use Illuminate\Support\Facades\Artisan;
use Illuminate\Support\Facades\Schedule;

Artisan::command('inspire', function () {
    $this->comment(Inspiring::quote());
})->purpose('Display an inspiring quote');

// US-11 / BR-08: recharge every wallet on the first day of the month,
// 00:00 WIB. withoutOverlapping + the Cache::lock inside the command
// itself both guard against double execution across instances.
Schedule::command('credits:recharge')
    ->monthlyOn(1, '00:00')
    ->timezone(config('credit.timezone', 'Asia/Jakarta'))
    ->withoutOverlapping(600)
    ->onOneServer();
