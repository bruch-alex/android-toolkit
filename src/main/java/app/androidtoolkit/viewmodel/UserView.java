package app.androidtoolkit.viewmodel;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public class UserView {
    private String id;
    private String name;

    public UserView(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
