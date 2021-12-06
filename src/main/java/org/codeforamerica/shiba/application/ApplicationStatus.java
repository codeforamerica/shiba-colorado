package org.codeforamerica.shiba.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.codeforamerica.shiba.output.Document;

@Data
@AllArgsConstructor
public class ApplicationStatus {

  private Document documentType;
  private Status status;
}