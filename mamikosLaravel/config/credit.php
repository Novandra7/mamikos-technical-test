<?php

declare(strict_types=1);

return [

    /*
    |--------------------------------------------------------------------------
    | Role Quotas
    |--------------------------------------------------------------------------
    |
    | Initial grant and monthly recharge target for each role that owns a
    | credit wallet. Owners never appear here — they have no wallet at all.
    |
    */
    'quota' => [
        'regular' => (int) env('CREDIT_REGULAR_QUOTA', 20),
        'premium' => (int) env('CREDIT_PREMIUM_QUOTA', 40),
    ],

    /*
    |--------------------------------------------------------------------------
    | Inquiry Cost
    |--------------------------------------------------------------------------
    |
    | How many credits a single availability inquiry costs (BR-02).
    |
    */
    'inquiry_cost' => (int) env('CREDIT_INQUIRY_COST', 5),

    /*
    |--------------------------------------------------------------------------
    | Recharge Strategy
    |--------------------------------------------------------------------------
    |
    | "reset" sets the balance back to the role quota every month (D-01,
    | default). "topup" adds the quota on top of the existing balance,
    | capped at max_balance.
    |
    */
    'recharge_strategy' => env('CREDIT_RECHARGE_STRATEGY', 'reset'),

    'max_balance' => (int) env('CREDIT_MAX_BALANCE', 200),

    'timezone' => env('CREDIT_SCHEDULE_TIMEZONE', 'Asia/Jakarta'),

    /*
    |--------------------------------------------------------------------------
    | Recharge Chunk Size
    |--------------------------------------------------------------------------
    |
    | Number of users processed per chunk during the monthly recharge job
    | (PERF-05).
    |
    */
    'recharge_chunk_size' => (int) env('CREDIT_RECHARGE_CHUNK_SIZE', 1000),
];
