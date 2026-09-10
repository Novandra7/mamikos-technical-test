<?php

declare(strict_types=1);

namespace App\Enums;

enum RoomType: string
{
    case PUTRA = 'putra';
    case PUTRI = 'putri';
    case CAMPUR = 'campur';

    /**
     * @return list<string>
     */
    public static function values(): array
    {
        return array_map(fn (self $type) => $type->value, self::cases());
    }
}
