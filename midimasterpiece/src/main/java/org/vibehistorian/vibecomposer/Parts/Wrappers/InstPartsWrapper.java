package org.vibehistorian.vibecomposer.Parts.Wrappers;

import org.vibehistorian.vibecomposer.Parts.ArpPart;
import org.vibehistorian.vibecomposer.Parts.BassPart;
import org.vibehistorian.vibecomposer.Parts.ChordPart;
import org.vibehistorian.vibecomposer.Parts.DrumPart;
import org.vibehistorian.vibecomposer.Parts.InstPart;
import org.vibehistorian.vibecomposer.Parts.MelodyPart;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD) // Use field-level annotations
@XmlSeeAlso({MelodyPartsWrapper.class,
        BassPartsWrapper.class,
        ChordPartsWrapper.class,
        ArpPartsWrapper.class,
        DrumPartsWrapper.class,

        MelodyPart.class,
        BassPart.class,
        ChordPart.class,
        ArpPart.class,
        DrumPart.class
})
public abstract class InstPartsWrapper<IP extends InstPart> {

    @XmlElementRefs({
            @XmlElementRef(name = "melodyPart", type = MelodyPart.class),
            @XmlElementRef(name = "bassPart", type = BassPart.class),
            @XmlElementRef(name = "chordPart", type = ChordPart.class),
            @XmlElementRef(name = "arpPart", type = ArpPart.class),
            @XmlElementRef(name = "drumPart", type = DrumPart.class)
    })
    private List<IP> parts = new ArrayList<>();

    public List<IP> getParts() {
        return parts;
    }

    public void setParts(List<? extends InstPart> parts) {
        this.parts = (List<IP>) parts;
    }

    public static Class<? extends InstPartsWrapper> getWrapperClass(int partNum) {
        switch (partNum) {
            case 0:
                return MelodyPartsWrapper.class;
            case 1:
                return BassPartsWrapper.class;
            case 2:
                return ChordPartsWrapper.class;
            case 3:
                return ArpPartsWrapper.class;
            case 4:
                return DrumPartsWrapper.class;
        }
        throw new IllegalArgumentException("PartNum out of scope: " + partNum);
    }

    public static InstPartsWrapper<?> forClass(Class<? extends InstPartsWrapper> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
