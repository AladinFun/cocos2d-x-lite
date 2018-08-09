#include "FrameData.h"

DRAGONBONES_NAMESPACE_BEGIN

ActionData::ActionData()
{
    _onClear();
}
ActionData::~ActionData()
{
    if (*_isInCleanUp) return;
    _onClear();
}

void ActionData::_onClear()
{
    type = ActionType::Play;
    bone = nullptr;
    slot = nullptr;

    std::get<0>(data).clear();
    std::get<1>(data).clear();
    std::get<2>(data).clear();
}

EventData::EventData()
{
    _onClear();
}
EventData::~EventData()
{
    if (*_isInCleanUp) return;
    _onClear();
}

void EventData::_onClear()
{
    type = EventType::Frame;
    name.clear();
    //data = nullptr;
    bone = nullptr;
    slot = nullptr;
}

AnimationFrameData::AnimationFrameData()
{
    _onClear();
}
AnimationFrameData::~AnimationFrameData()
{
    if (*_isInCleanUp) return;
    _onClear();
}

void AnimationFrameData::_onClear()
{
    FrameData::_onClear();
}

BoneFrameData::BoneFrameData()
{
    _onClear();
}
BoneFrameData::~BoneFrameData()
{
    if (*_isInCleanUp) return;
    _onClear();
}

void BoneFrameData::_onClear()
{
    TweenFrameData::_onClear();

    tweenScale = false;
    tweenRotate = 0;
    parent = nullptr;
    transform.identity();
}

ColorTransform SlotFrameData::DEFAULT_COLOR;
ColorTransform * SlotFrameData::generateColor()
{
    return new ColorTransform();
}

SlotFrameData::SlotFrameData() :
    color(nullptr)
{
    _onClear();
}
SlotFrameData::~SlotFrameData()
{
    if (*_isInCleanUp) return;
    _onClear();
}

void SlotFrameData::_onClear()
{
    TweenFrameData::_onClear();

    displayIndex = 0;
    zOrder = 0;

    if (color)
    {
        if (color != &DEFAULT_COLOR)
        {
            delete color;
        }

        color = nullptr;
    }
}

ExtensionFrameData::ExtensionFrameData()
{
    _onClear();
}
ExtensionFrameData::~ExtensionFrameData()
{
    if (*_isInCleanUp) return;
    _onClear();
}

void ExtensionFrameData::_onClear()
{
    TweenFrameData::_onClear();

    type = ExtensionType::FFD;
    tweens.clear();
    keys.clear();
}

DRAGONBONES_NAMESPACE_END
